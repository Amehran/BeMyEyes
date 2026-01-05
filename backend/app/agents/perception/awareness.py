from app.agents.core.base import BaseAgent
from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.services.llm_gateway import llm_gateway
import json

class AwarenessAgent(BaseAgent):
    """
    The Awareness Agent (Concept V2).
    
    Focus: Spatial Understanding and Anchoring.
    Instead of generic descriptions, it anchors objects to relative locations
    (e.g., "to your left", "10 o'clock").
    
    Future: Will integrate Session Memory to remember previous landmarks.
    """
    
    SYSTEM_PROMPT = """
    You are the SPATIAL AWARENESS AGENT for a blind user.
    Your mission is NOT just to describe the scene, but to ANCHOR key elements spatially.
    
    RELATIVITY RULES:
    1. Assume the user is holding the camera at chest/head level facing forward.
    2. Use CLOCK FACE directions (12 o'clock is straight ahead, 3 is right, 9 is left).
    3. Estimate DISTANCE (steps or meters) if possible.
    
    OUTPUT PROTOCOL:
    1. First, verify the "Scene Context" (e.g., Living Room, Sidewalk).
    2. Review PREVIOUS CONTEXT to maintain continuity (e.g. if user just saw a door, refer to it).
    3. Then, list the 3-4 DOMINANT objects/paths with their anchors.
    
    JSON OUTPUT FORMAT:
    {
      "context": "Modern Office Corridor",
      "anchors": [
        {"object": "Glass Door", "loc": "12 o'clock", "dist": "5m", "note": "Same door as before"}
      ],
      "summary": "You are still in the corridor. The glass door is now closer, 5 meters ahead."
    }
    """

    async def analyze(self, request: AnalysisRequest, history: list = []) -> AnalysisResponse:
        from app.services.memory import memory_service

        # 1. Retrieve Long-Term Memory
        user_id = request.user_id or "default_user"
        query_text = request.audio_query or "Describe the surroundings"
        
        recalled_memories = await memory_service.search_memories(user_id, query_text)
        memory_text = "No relevant past memories."
        if recalled_memories:
            memory_text = "\\n".join([f"- {m}" for m in recalled_memories])

        # 2. Build Context String
        history_text = "No previous context."
        if history:
            history_text = "\\n".join([f"- User: {h.get('query','?')}\\n  Agent: {h.get('response','')}" for h in history[-3:]])
        
        telemetry_info = ""
        if request.telemetry:
            telemetry_info = f"USER TELEMETRY: Heading={request.telemetry.heading} deg (0=North), Pitch={request.telemetry.pitch} deg."

        full_prompt = f"{self.SYSTEM_PROMPT}\\n\\n{telemetry_info}\\n\\nLONG-TERM MEMORY:\\n{memory_text}\\n\\nPREVIOUS SESSION CONTEXT:\\n{history_text}"

        # 3. Call LLM
        raw_response = await llm_gateway.generate_response(
            system_prompt=full_prompt,
            image_data=request.image_base64,
            model_name="gemini-flash-latest"
        )
        
        try:
            # 4. Parse JSON
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            # 5. Format Output & Store Memory
            summary_text = data.get("summary", "Scene analysis complete.")
            
            # Store this interaction as a memory
            # We store the summary which contains the "facts"
            await memory_service.store_memory(user_id, f"Context: {data.get('context')}. Detail: {summary_text}")
            
            return AnalysisResponse(
                agent_used="AwarenessAgent",
                actions=[
                    Action(type="TTS", content=summary_text)
                ]
            )
            
        except Exception as e:
            # Fallback for parsing errors
            return AnalysisResponse(
                agent_used="AwarenessAgent",
                actions=[
                    Action(type="TTS", content=f"I see the scene, but couldn't map it precisely. Raw: {raw_response[:50]}...")
                ]
            )
            
awareness_agent = AwarenessAgent()

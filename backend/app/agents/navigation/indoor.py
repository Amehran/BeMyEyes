from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.core.base import BaseAgent
import json

class IndoorNavigationAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are an Indoor Navigation Assistant for a blind user.
    Your mission is MICRO-NAVIGATION in controlled environments (Home, Office, Mall).
    
    Focus on:
    - Doors (Open/Closed, Handle location)
    - Hallways (Clear path, Obstacles like chairs, boxes)
    - Furniture (Tables, Sofas - approximate location)
    - Elevators / Stairs
    
    Modes:
    1. PILOT MODE (Cluttered): "Chair 2 steps ahead. Side step right." (Short, imperative)
    2. GUIDANCE MODE (Clear): "Hallway is clear. Walk straight approx 10 meters." (Descriptive)
    
    Output strictly valid JSON:
    { "speech": "Walk forward 5 steps.", "haptic": "INFO_PULSE" }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        system_prompt = self.SYSTEM_PROMPT
        if request.telemetry and request.telemetry.speed_mps > 1.0:
            system_prompt += "\nUser is moving fast. Be concise. Provide Pilot Mode instructions."
        
        # Check if user asked something specific
        if request.audio_query:
             system_prompt += f"\nUser Question: '{request.audio_query}'. Answer this specifically while navigating."

        raw_response = await llm_gateway.generate_response(
            system_prompt=system_prompt,
            image_data=request.image_base64,
            model_name="gemini-flash-latest"
        )
        
        # Safe Parsing
        try:
            # Clean fences if LLM outputs markdown
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="IndoorNavigationAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", "Move cautiously.")),
                    Action(type="HAPTIC", content=data.get("haptic", "CAUTION"))
                ]
            )
        except Exception:
            # Fallback if LLM halluncinates format
            return AnalysisResponse(
                agent_used="IndoorNavigationAgent",
                actions=[Action(type="TTS", content=raw_response[:100])] # Just speak the raw text
            )

indoor_navigation_agent = IndoorNavigationAgent()

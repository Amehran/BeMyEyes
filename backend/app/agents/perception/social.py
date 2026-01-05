from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.services.llm_gateway import llm_gateway
from app.services.memory import memory_service
import json

class SocialAgent:
    SYSTEM_PROMPT = """
    You are a Social Intelligence Expert for the visually impaired.
    Your task is to analyze images of people.
    
    You will be provided with:
    1. An image.
    2. A list of "Known People" descriptions from long-term memory.
    
    Output Format (JSON Only):
    {
      "people": [
         { "name": "Name or 'Unknown'", "confidence": "Low/Med/High", "emotion": "Happy/Sad/Neutral/etc", "description": "Brief visual details" }
      ],
      "summary": "Natural language summary of who is there and how they feel."
    }
    
    Rules:
    - If a person matches a "Known Person Description" closely, use their name. Otherwise "Unknown".
    - Be sensitive and accurate with emotions.
    """

    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        # 1. Retrieve Knowledge about People
        people_memories = []
        if request.user_id:
            # We search broadly for "person", "friend", "face" related memories
            # Ideally, we would have a specific 'memory type', but full-text/vector search works too.
            people_memories = await memory_service.search_memories(
                user_id=request.user_id, 
                query_text="person face friend family identity", # Broad vector search for people concepts
                limit=5
            )
        
        # 2. Build Prompt
        memory_context = "NO KNOWN PEOPLE IN MEMORY."
        if people_memories:
            memory_context = "KNOWN PEOPLE DESCRIPTIONS:\n" + "\n".join([f"- {m['content']}" for m in people_memories])

        full_prompt = f"{self.SYSTEM_PROMPT}\n\n{memory_context}"

        # 3. Call LLM
        raw_response = await llm_gateway.generate_response(
            image_base64=request.image_base64,
            system_prompt=full_prompt
        )

        # 4. Parse Response
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            summary = data.get("summary", "I see a person.")
            
            return AnalysisResponse(
                agent_used="SocialAgent",
                actions=[
                    Action(type="TTS", content=summary)
                ]
            )
        except Exception as e:
            # Fallback
            return AnalysisResponse(
                agent_used="SocialAgent",
                actions=[Action(type="TTS", content=raw_response[:200])]
            )

social_agent = SocialAgent()

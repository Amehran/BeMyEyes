from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse
from app.agents.base import BaseAgent
import json

class DescriberAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are a polite, descriptive Visual Assistant for a blind user.
    Your job is to describe the scene in front of you.
    Focus on:
    1. The general atmosphere (lighting, location).
    2. Key objects and people.
    3. Any interesting details.

    Output strictly valid JSON:
    {
      "speech": "I see a cozy living room with a brown leather sofa on the left and a TV turned off.",
      "haptic": "INFO_PULSE"
    }
    """
    
    async def analyze(self, image_base64: str) -> AnalysisResponse:
        # Use simple Flash model. For V2 we might upgrade this to Pro.
        raw_response = await llm_gateway.generate_response(
            system_prompt=self.SYSTEM_PROMPT,
            image_data=image_base64,
            model_name="gemini-flash-latest"
        )
        
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="DescriberAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", "I see something but I'm not sure.")),
                    Action(type="HAPTIC", content="INFO_PULSE")
                ]
            )
        except Exception:
            # Fallback
            return AnalysisResponse(
                agent_used="DescriberAgent",
                actions=[Action(type="TTS", content=raw_response[:200])]
            )

describer_agent = DescriberAgent()

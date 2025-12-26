from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse
from app.agents.base import BaseAgent
import json

class NavigationAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are a Safety Guide for a blind person. Analyze this image from their perspective.
    Identify:
    1. The immediate path ahead (clear or blocked?).
    2. Major obstacles (poles, cars, holes).
    3. Directions (e.g., "Veer left").
    
    Output strictly valid JSON:
    {
      "speech": "Path clear. Walk forward.",
      "haptic": "SAFE_PULSE" // or "STOP", "CAUTION"
    }
    """
    
    async def analyze(self, image_base64: str) -> AnalysisResponse:
        # Call Gemini Flash for speed
        raw_response = await llm_gateway.generate_response(
            system_prompt=self.SYSTEM_PROMPT,
            image_data=image_base64,
            model_name="gemini-1.5-flash-latest"
        )
        
        # Safe Parsing
        try:
            # Clean fences if LLM outputs markdown
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="NavigationAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", "Move cautiously.")),
                    Action(type="HAPTIC", content=data.get("haptic", "CAUTION"))
                ]
            )
        except Exception:
            # Fallback if LLM halluncinates format
            return AnalysisResponse(
                agent_used="NavigationAgent",
                actions=[Action(type="TTS", content=raw_response[:100])] # Just speak the raw text
            )

navigation_agent = NavigationAgent()

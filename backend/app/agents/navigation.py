from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.base import BaseAgent
import json

class NavigationAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are a polite Navigation Assistant for a blind user.
    Identify safe paths, obstacles, and signs.
    Output strictly valid JSON:
    { "speech": "Walk forward 5 steps.", "haptic": "INFO_PULSE" }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        system_prompt = self.SYSTEM_PROMPT
        if request.telemetry and request.telemetry.speed_mps > 1.0:
            system_prompt += "\nUser is moving fast. Be concise and warn of immediate dangers."

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

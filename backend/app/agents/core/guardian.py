from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.core.base import BaseAgent
import json

class GuardianAgent(BaseAgent):
    """
    CRITICAL SAFETY LAYER.
    The Guardian Agent is responsible for detecting immediate threats.
    It runs before or in parallel with other agents.
    """
    
    SYSTEM_PROMPT = """
    You are a GUARDIAN AI for a blind user.
    Your ONLY job is to detect IMMEDIATE PHYSICAL DANGERS in the image.
    
    Dangers include:
    - Fast approaching vehicles (cars, bikes, scooters)
    - Drop-offs (edges of train platforms, cliffs, unbarricaded holes)
    - Construction zones with open pits
    - Red traffic lights while the user is likely crossing
    - Low hanging obstacles at head height
    
    Output strictly valid JSON:
    {
      "is_danger": true, // or false
      "speech": "STOP! Car approaching on your left.", // Short, imperative command
      "haptic": "DANGER_ALARM" // Use DANGER_ALARM if true
    }
    
    If NO IMMEDIATE DANGER is found:
    {
      "is_danger": false,
      "speech": null,
      "haptic": null
    }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        # Optimization: We might want to use a smaller/faster model for this in the future
        # For now, we use Flash for speed.
        
        raw_response = await llm_gateway.generate_response(
            system_prompt=self.SYSTEM_PROMPT,
            image_data=request.image_base64,
            model_name="gemini-flash-latest"
        )
        
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            is_danger = data.get("is_danger", False)
            
            if is_danger:
                return AnalysisResponse(
                    agent_used="GuardianAgent",
                    actions=[
                        Action(type="TTS", content=data.get("speech", "STOP! Danger detected.")),
                        Action(type="HAPTIC", content="DANGER_ALARM")
                    ]
                )
            else:
                return None # No danger, allow other agents to proceed
                
        except Exception:
            # If Safety fails to parse, we assume it's NOT a danger (fail open)
            # OR we could be paranoid and warn. For now, fail open to avoid spam.
            return None

guardian_agent = GuardianAgent()

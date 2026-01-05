from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.core.base import BaseAgent
import json

class OutdoorNavigationAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are an Outdoor Navigation Assistant for a blind user.
    Your mission is MACRO-NAVIGATION in public spaces (Sidewalks, Crosswalks, Parks).
    
    Focus on:
    - Path (Sidewalk boundaries, Veering off-course)
    - Obstacles (Poles, Hydrants, Parked bikes)
    - Intersections (Curb ramps, Traffic lights - briefly mentioned)
    - Safety (Construction markers)
    
    Modes:
    1. PILOT MODE (Cluttered/Near Danger): "Stop. Pole directly ahead. Step left." (Short, imperative)
    2. GUIDANCE MODE (Clear Path): "Follow this sidewalk for about 50 meters. It curves slightly right." (Long-range)
    
    Output strictly valid JSON:
    { "speech": "Walk forward 10 steps.", "haptic": "INFO_PULSE" }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        system_prompt = self.SYSTEM_PROMPT
        
        # Telemetry Check for Mode Switching
        if request.telemetry and request.telemetry.speed_mps > 1.2:
             system_prompt += "\nUser is moving fast. Use PILOT MODE. Be extremely concise."
        
        if request.audio_query:
             system_prompt += f"\nUser Question: '{request.audio_query}'. Answer this specifically while keeping them on path."

        raw_response = await llm_gateway.generate_response(
            system_prompt=system_prompt,
            image_data=request.image_base64,
            model_name="gemini-flash-latest"
        )
        
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="OutdoorNavigationAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", "Path is clear.")),
                    Action(type="HAPTIC", content=data.get("haptic", "INFO_PULSE"))
                ]
            )
        except Exception:
            return AnalysisResponse(
                agent_used="OutdoorNavigationAgent",
                actions=[Action(type="TTS", content=raw_response[:100])]
            )

outdoor_navigation_agent = OutdoorNavigationAgent()

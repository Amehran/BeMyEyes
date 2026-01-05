from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.core.base import BaseAgent
import json

class ObjectFinderAgent(BaseAgent):
    """
    Specialized Agent for finding specific objects.
    "Where is the [target]?"
    """
    
    SYSTEM_PROMPT = """
    You are a Visual Object Finder for a blind user.
    Your mission: Locate the SPECIFIC TARGET OBJECT requested by the user.
    
    Target: {target}
    
    Output format:
    1. If found: Give precise CLOCK-FACE direction and approximate distance.
       Example: "The keys are at 2 o'clock, about 1 meter away on the table."
    2. If NOT found: Say "I don't see the [target] yet. Try panning [left/right]."
    
    Output strictly valid JSON:
    { 
      "found": true,
      "speech": "Target located at 12 o'clock.",
      "haptic": "SUCCESS_PULSE" // Use ERROR_PULSE if not found
    }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        target = request.looking_for if request.looking_for else "the object you asked for"
        
        # If the user asked via audio "Where are my keys?", we might need to extract the target from audio_query 
        # if 'looking_for' is not explicitly set. For now, we trust 'looking_for' or fallback to audio query.
        if not request.looking_for and request.audio_query:
             target = "the object mentioned in: " + request.audio_query

        # Use replace instead of format to avoid KeyError with JSON braces
        formatted_prompt = self.SYSTEM_PROMPT.replace("{target}", target)

        raw_response = await llm_gateway.generate_response(
            system_prompt=formatted_prompt,
            image_data=request.image_base64,
            model_name="gemini-flash-latest"
        )
        
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="ObjectFinderAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", f"I am looking for {target}.")),
                    Action(type="HAPTIC", content=data.get("haptic", "INFO_PULSE"))
                ]
            )
        except Exception:
            return AnalysisResponse(
                agent_used="ObjectFinderAgent",
                actions=[Action(type="TTS", content=raw_response[:100])]
            )

object_finder_agent = ObjectFinderAgent()

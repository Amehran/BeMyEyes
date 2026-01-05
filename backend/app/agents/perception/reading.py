from app.services.llm_gateway import llm_gateway
from app.schemas.request_response import Action, AnalysisResponse, AnalysisRequest
from app.agents.core.base import BaseAgent
import json

class ReadingAgent(BaseAgent):
    SYSTEM_PROMPT = """
    You are an Optical Character Recognition (OCR) Specialist.
    Your job is to read text from the image for a blind user.
    Rules:
    1. Read exact text verbatim if it looks like a document/sign.
    2. If the text is long (menu/article), summarize the key sections.
    3. If no text is found, clearly state that.
    
    Output strictly valid JSON:
    {
      "speech": "The sign says: 'Stop, Road Closed'.",
      "haptic": "INFO_PULSE" // Use INFO_PULSE for generic text
    }
    """
    
    async def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        system_prompt = self.SYSTEM_PROMPT
        if request.audio_query:
             system_prompt += f"\nUser Question: '{request.audio_query}'. If looking for specific text, focus on that."

        # Using Gemini 1.5 Flash for now (it has excellent OCR and is cheaper).
        # We can switch to Pro later if accuracy is low.
        raw_response = await llm_gateway.generate_response(
            system_prompt=system_prompt,
            image_data=request.image_base64,
            model_name="gemini-flash-latest" 
        )
        
        try:
            clean_json = raw_response.replace("```json", "").replace("```", "").strip()
            data = json.loads(clean_json)
            
            return AnalysisResponse(
                agent_used="ReadingAgent",
                actions=[
                    Action(type="TTS", content=data.get("speech", "No text found.")),
                    Action(type="HAPTIC", content=data.get("haptic", "INFO_PULSE"))
                ]
            )
        except Exception:
            return AnalysisResponse(
                agent_used="ReadingAgent",
                actions=[Action(type="TTS", content=raw_response[:200])]
            )

reading_agent = ReadingAgent()

import google.generativeai as genai
from app.core.config import settings
import logging

logger = logging.getLogger("be-my-eyes-backend")

class LLMGateway:
    def __init__(self):
        if not settings.GEMINI_API_KEY:
            logger.warning("GEMINI_API_KEY is missing. AI features will fail.")
        else:
            genai.configure(api_key=settings.GEMINI_API_KEY)
            
    async def generate_response(self, system_prompt: str, image_data: str = None, model_name: str = "gemini-1.5-flash") -> str:
        try:
            model = genai.GenerativeModel(model_name)
            
            content = [system_prompt]
            if image_data and len(image_data) > 100: # Simple check for real data
                content.append({
                    "mime_type": "image/jpeg",
                    "data": image_data
                })
            else:
                logger.info("No valid image data provided. Running text-only mode.")
                
            response = await model.generate_content_async(content)
            return response.text
        except Exception as e:
            logger.error(f"Gemini API Error: {e}")
            return "Thinking..." # Fallback or error re-raise
            
llm_gateway = LLMGateway()

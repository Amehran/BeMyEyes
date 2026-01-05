import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from app.services.llm_gateway import LLMGateway

@pytest.mark.asyncio
async def test_generate_response_handling():
    with patch("app.services.llm_gateway.genai.GenerativeModel") as mock_model_cls:
        # Mock the async generate_content_async
        mock_instance = MagicMock()
        mock_response = MagicMock()
        mock_response.text = "AI Response"
        
        # Async mock for the generation call
        mock_instance.generate_content_async = AsyncMock(return_value=mock_response)
        
        mock_model_cls.return_value = mock_instance
        
        gateway = LLMGateway()
        result = await gateway.generate_response("prompt")
        
        assert result == "AI Response"
        mock_instance.generate_content_async.assert_called_once()

@pytest.mark.asyncio
async def test_generate_response_error_graceful_fallback():
    with patch("app.services.llm_gateway.genai.GenerativeModel") as mock_model_cls:
        mock_instance = MagicMock()
        # Raise exception
        mock_instance.generate_content_async = AsyncMock(side_effect=Exception("API Down"))
        mock_model_cls.return_value = mock_instance
        
        gateway = LLMGateway()
        result = await gateway.generate_response("prompt")
        
        assert result == "Thinking..."

@pytest.mark.asyncio
async def test_get_embedding_error_handling():
    with patch("app.services.llm_gateway.genai.embed_content") as mock_embed:
        mock_embed.side_effect = Exception("Embed Error")
        
        gateway = LLMGateway()
        result = await gateway.get_embedding("text")
        
        assert result == []

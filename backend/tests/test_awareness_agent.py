import pytest
from unittest.mock import AsyncMock, patch
from app.agents.perception.awareness import awareness_agent
from app.schemas.request_response import AnalysisRequest

@pytest.mark.asyncio
async def test_awareness_agent_spatial_response():
    # Mock Request
    request = AnalysisRequest(
        user_intent="GENERAL",
        image_base64="fake_base64_data",
        image_width=100,
        image_height=100
    )
    
    # Mock LLM Response (Correct JSON)
    mock_json_response = """
    ```json
    {
      "context": "Living Room",
      "anchors": [
        {"object": "Sofa", "loc": "12 o'clock", "dist": "3m", "note": "Blue"}
      ],
      "summary": "You are in the Living Room. Sofa is straight ahead 3 meters."
    }
    ```
    """
    
    with patch("app.services.llm_gateway.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_json_response
        
        # ACT
        response = await awareness_agent.analyze(request)
        
        # ASSERT
        assert response.agent_used == "AwarenessAgent"
        assert len(response.actions) == 1
        assert response.actions[0].type == "TTS"
        assert "Sofa is straight ahead" in response.actions[0].content

@pytest.mark.asyncio
async def test_awareness_agent_parsing_failure():
    # Test Robustness against bad LLM output
    request = AnalysisRequest(image_base64="data")
    
    with patch("app.services.llm_gateway.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = "Not JSON"
        
        response = await awareness_agent.analyze(request)
        
        # Should fallback gracefully
        assert response.agent_used == "AwarenessAgent"
        assert "couldn't map it" in response.actions[0].content

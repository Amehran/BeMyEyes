import pytest
from unittest.mock import AsyncMock, patch
from app.agents.navigation import NavigationAgent

@pytest.mark.asyncio
async def test_navigation_agent_returns_structured_response():
    # 1. Arrange: Mock the LLM Gateway to return valid JSON
    mock_json_response = """
    ```json
    {
      "speech": "Watch step.",
      "haptic": "CAUTION"
    }
    ```
    """
    
    with patch("app.agents.navigation.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_json_response
        
        agent = NavigationAgent()
        
        # 2. Act
        response = await agent.analyze("dummy_base64_image")
        
        # 3. Assert
        assert response.agent_used == "NavigationAgent"
        assert len(response.actions) == 2
        assert response.actions[0].type == "TTS"
        assert response.actions[0].content == "Watch step."
        assert response.actions[1].type == "HAPTIC"
        assert response.actions[1].content == "CAUTION"

@pytest.mark.asyncio
async def test_navigation_agent_handles_bad_json_gracefully():
    # 1. Arrange: Mock LLM returns garbage (not JSON)
    mock_garbage = "I am not JSON, I am just chatty text."
    
    with patch("app.agents.navigation.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_garbage
        
        agent = NavigationAgent()
        
        # 2. Act
        response = await agent.analyze("dummy_base64")
        
        # 3. Assert
        assert response.agent_used == "NavigationAgent"
        # It should fallback to speaking the raw text
        assert response.actions[0].type == "TTS"
        assert "I am not JSON" in response.actions[0].content

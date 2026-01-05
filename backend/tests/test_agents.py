import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from app.agents.core.guardian import GuardianAgent
from app.agents.navigation.indoor import IndoorNavigationAgent
from app.agents.navigation.outdoor import OutdoorNavigationAgent
from app.agents.perception.detector import ObjectFinderAgent
from app.schemas.request_response import AnalysisRequest, Telemetry

@pytest.mark.asyncio
async def test_guardian_agent_detects_danger():
    mock_response = """
    ```json
    {
      "is_danger": true,
      "speech": "STOP! Car approaching.",
      "haptic": "DANGER_ALARM"
    }
    ```
    """
    with patch("app.agents.core.guardian.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_response
        agent = GuardianAgent()
        
        request = AnalysisRequest(image_base64="dummy")
        response = await agent.analyze(request)
        
        assert response is not None
        assert response.agent_used == "GuardianAgent"
        assert response.actions[0].content == "STOP! Car approaching."

@pytest.mark.asyncio
async def test_indoor_navigation_agent_pilot_mode():
    mock_response = """
    ```json
    { "speech": "Chair ahead. Step right.", "haptic": "CAUTION" }
    ```
    """
    with patch("app.agents.navigation.indoor.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_response
        agent = IndoorNavigationAgent()
        
        request = AnalysisRequest(image_base64="dummy", telemetry=Telemetry(speed_mps=1.5, location_type="INDOOR"))
        response = await agent.analyze(request)
        
        assert response.agent_used == "IndoorNavigationAgent"
        # Verify prompt modification for speed (implicit check via mock call args if needed, but output is enough)
        assert response.actions[0].content == "Chair ahead. Step right."

@pytest.mark.asyncio
async def test_object_finder_agent_search():
    mock_response = """
    ```json
    { 
      "found": true, 
      "speech": "Keys at 2 o'clock.", 
      "haptic": "SUCCESS_PULSE" 
    }
    ```
    """
    with patch("app.agents.perception.detector.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_response
        agent = ObjectFinderAgent()
        
        request = AnalysisRequest(image_base64="dummy", looking_for="keys")
        response = await agent.analyze(request)
        
        assert response.agent_used == "ObjectFinderAgent"
        mock_generate.assert_called_once()
        args, kwargs = mock_generate.call_args
        assert "keys" in kwargs["system_prompt"] # Check system prompt contains target

@pytest.mark.asyncio
async def test_indoor_navigation_agent_fast_speed():
    mock_response = """
    ```json
    { "speech": "STOP. Wall.", "haptic": "STOP_ALARM" }
    ```
    """
    with patch("app.agents.navigation.indoor.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_response
        agent = IndoorNavigationAgent()
        
        # User moving > 1.0 mps
        request = AnalysisRequest(
            image_base64="dummy", 
            telemetry=Telemetry(speed_mps=1.5, location_type="INDOOR")
        )
        response = await agent.analyze(request)
        
        assert response.agent_used == "IndoorNavigationAgent"
        
        # Verify that prompt contains the Speed hint
        args, kwargs = mock_generate.call_args
        assert "User is moving fast" in kwargs["system_prompt"]

@pytest.mark.asyncio
async def test_reading_agent_ocr():
    from app.agents.perception.reading import ReadingAgent
    
    mock_response = """
    ```json
    { "speech": "The sign says 'Open 24 Hours'.", "haptic": "INFO_PULSE" }
    ```
    """
    with patch("app.agents.perception.reading.llm_gateway.generate_response", new_callable=AsyncMock) as mock_generate:
        mock_generate.return_value = mock_response
        agent = ReadingAgent()
        
        request = AnalysisRequest(image_base64="dummy", user_intent="READING")
        response = await agent.analyze(request)
        
        assert response.agent_used == "ReadingAgent"
        assert "Open 24 Hours" in response.actions[0].content

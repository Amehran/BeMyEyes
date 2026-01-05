import pytest
from unittest.mock import AsyncMock
from app.services.orchestrator import OrchestratorService
from app.schemas.request_response import AnalysisRequest, Telemetry

@pytest.mark.asyncio
async def test_orchestrator_guardian_intervention():
    # Arrange
    mock_guardian = AsyncMock()
    # Guardian returns a Response (Danger)
    mock_guardian.analyze.return_value = "DANGER_RESPONSE_OBJECT" 
    
    orchestrator = OrchestratorService(guardian_agent_instance=mock_guardian)
    
    request = AnalysisRequest(image_base64="dummy")
    
    # Act
    response = await orchestrator.process_request(request)
    
    # Assert
    assert response == "DANGER_RESPONSE_OBJECT"
    mock_guardian.analyze.assert_called_once()

@pytest.mark.asyncio
async def test_orchestrator_routes_to_outdoor():
    # Arrange
    mock_guardian = AsyncMock()
    mock_guardian.analyze.return_value = None # No danger
    
    mock_outdoor = AsyncMock()
    mock_outdoor.analyze.return_value = "OUTDOOR_RESPONSE"
    
    orchestrator = OrchestratorService(
        guardian_agent_instance=mock_guardian,
        outdoor_agent_instance=mock_outdoor
    )
    
    # Telemetry says OUTDOOR -> Should route to Outdoor Agent
    request = AnalysisRequest(
        image_base64="dummy", 
        user_intent="NAVIGATION",
        telemetry=Telemetry(location_type="OUTDOOR")
    )
    
    # Act
    response = await orchestrator.process_request(request)
    
    # Assert
    assert response == "OUTDOOR_RESPONSE"
    mock_outdoor.analyze.assert_called_once()

@pytest.mark.asyncio
async def test_orchestrator_routes_to_finder():
    # Arrange
    mock_guardian = AsyncMock()
    mock_guardian.analyze.return_value = None
    
    mock_finder = AsyncMock()
    mock_finder.analyze.return_value = "FINDER_RESPONSE"
    
    orchestrator = OrchestratorService(
        guardian_agent_instance=mock_guardian,
        finder_agent_instance=mock_finder
    )
    
    # Intent is SEARCH
    request = AnalysisRequest(image_base64="dummy", user_intent="SEARCH")
    
    # Act
    response = await orchestrator.process_request(request)
    
    # Assert
    assert response == "FINDER_RESPONSE"
    mock_finder.analyze.assert_called_once()

@pytest.mark.asyncio
async def test_orchestrator_implicit_search_routing():
    # Arrange
    mock_guardian = AsyncMock()
    mock_guardian.analyze.return_value = None
    
    mock_finder = AsyncMock()
    mock_finder.analyze.return_value = "FINDER_RESPONSE"
    
    orchestrator = OrchestratorService(
        guardian_agent_instance=mock_guardian,
        finder_agent_instance=mock_finder
    )
    
    # Audio query contains "find" -> Should route to Finder
    request = AnalysisRequest(
        image_base64="dummy", 
        user_intent="GENERAL", 
        audio_query="Where are my keys?"
    )
    
    # Act
    response = await orchestrator.process_request(request)
    
    # Assert
    assert response == "FINDER_RESPONSE"
    mock_finder.analyze.assert_called_once()

@pytest.mark.asyncio
async def test_orchestrator_routes_to_awareness():
    # Arrange
    from app.schemas.request_response import AnalysisResponse, Action
    
    mock_guardian = AsyncMock()
    mock_guardian.analyze.return_value = None
    
    mock_awareness = AsyncMock()
    # Return a valid AnalysisResponse object
    mock_awareness.analyze.return_value = AnalysisResponse(
        agent_used="AwarenessAgent",
        actions=[Action(type="TTS", content="Room described.")]
    )
    
    orchestrator = OrchestratorService(
        guardian_agent_instance=mock_guardian,
        awareness_agent_instance=mock_awareness
    )
    
    # Intent is GENERAL, query is descriptive
    request = AnalysisRequest(
        image_base64="dummy", 
        user_intent="GENERAL", 
        audio_query="Describe the room"
    )
    
    # Act
    response = await orchestrator.process_request(request)
    
    # Assert
    assert response.agent_used == "AwarenessAgent"
    mock_awareness.analyze.assert_called_once()

@pytest.mark.asyncio
async def test_orchestrator_voice_command_interception():
    orchestrator = OrchestratorService()
    
    # Simulate "Switch to Outdoor Mode"
    request = AnalysisRequest(
        image_base64="dummy", 
        audio_query="Switch to outdoor mode"
    )
    
    response = await orchestrator.process_request(request)
    
    assert response.agent_used == "Orchestrator"
    assert response.actions[0].type == "SETTING_UPDATE"
    assert response.actions[0].content == "OUTDOOR=TRUE"

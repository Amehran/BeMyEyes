import pytest
from unittest.mock import AsyncMock
from app.services.orchestrator import OrchestratorService
from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action

@pytest.mark.asyncio
async def test_orchestrator_routes_to_social():
    # Setup
    mock_social = AsyncMock()
    mock_social.analyze.return_value = AnalysisResponse(agent_used="SocialAgent", actions=[])
    
    # Pass generic mocks for others to avoid instantiation issues
    mock_guardian = AsyncMock()
    mock_guardian.analyze.return_value = None # No danger
    
    orchestrator = OrchestratorService(
        guardian_agent_instance=mock_guardian,
        social_agent_instance=mock_social,
        indoor_agent_instance=AsyncMock(),
        outdoor_agent_instance=AsyncMock(),
        reading_agent_instance=AsyncMock(),
        describer_agent_instance=AsyncMock(),
        finder_agent_instance=AsyncMock(),
        awareness_agent_instance=AsyncMock()
    )
    
    req = AnalysisRequest(
        image_base64="dummy",
        user_intent="AUTO",
        audio_query="Who is in front of me?"
    )
    
    # Act
    await orchestrator.process_request(req)
    
    # Assert
    mock_social.analyze.assert_called_once()

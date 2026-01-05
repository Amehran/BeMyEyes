from fastapi.testclient import TestClient
from unittest.mock import AsyncMock, patch
from app.main import app

client = TestClient(app)

def test_health_check_endpoint():
    response = client.get("/")
    assert response.status_code == 200
    assert response.json() == {"status": "active", "model": "gemini-1.5-flash"}

@patch("app.main.orchestrator.process_request", new_callable=AsyncMock)
def test_analyze_endpoint_success(mock_process):
    # Mock Orchestrator response
    from app.schemas.request_response import AnalysisResponse, Action
    mock_response = AnalysisResponse(
        agent_used="TestAgent",
        actions=[Action(type="TTS", content="Hello")]
    )
    mock_process.return_value = mock_response

    payload = {
        "image_base64": "dummy",
        "user_intent": "AUTO"
    }
    
    response = client.post("/api/analyze", json=payload)
    
    assert response.status_code == 200
    data = response.json()
    assert data["agent_used"] == "TestAgent"
    assert data["actions"][0]["content"] == "Hello"

from fastapi import APIRouter
from app.schemas.request_response import AnalysisRequest, AnalysisResponse, Action
from app.services.orchestrator import orchestrator

router = APIRouter()

@router.post("/analyze", response_model=AnalysisResponse)
async def analyze_scene(request: AnalysisRequest):
    return await orchestrator.process_request(request)

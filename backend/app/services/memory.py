from supabase import create_client, Client
from app.core.config import settings
import logging

logger = logging.getLogger(__name__)

class MemoryService:
    def __init__(self):
        self.client: Client | None = None
        self._initialize()

    def _initialize(self):
        if settings.SUPABASE_URL and settings.SUPABASE_KEY and "YOUR_SUPABASE" not in settings.SUPABASE_KEY:
            try:
                self.client = create_client(settings.SUPABASE_URL, settings.SUPABASE_KEY)
                logger.info("MemoryService: Connected to Supabase.")
            except Exception as e:
                logger.error(f"MemoryService: Failed to connect to Supabase: {e}")
        else:
            logger.warning("MemoryService: Supabase credentials missing or invalid. Memory disabled.")

    def check_health(self) -> bool:
        return self.client is not None

memory_service = MemoryService()

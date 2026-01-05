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

    async def store_memory(self, user_id: str, content: str):
        if not self.client: return
        
        # 1. Generate Vector
        from app.services.llm_gateway import llm_gateway
        embedding = await llm_gateway.get_embedding(content)
        if not embedding: return

        # 2. Insert into Supabase
        try:
            self.client.table("memories").insert({
                "user_id": user_id,
                "content": content,
                "embedding": embedding
            }).execute()
            logger.info(f"Memory stored for user {user_id}")
        except Exception as e:
            logger.error(f"Failed to store memory: {e}")

    async def search_memories(self, user_id: str, query: str, limit: int = 3) -> list[str]:
        if not self.client: return []
        
        # 1. Generate Query Vector
        from app.services.llm_gateway import llm_gateway
        query_embedding = await llm_gateway.get_embedding(query)
        if not query_embedding: return []

        # 2. RPC Call for Similarity Search
        try:
            response = self.client.rpc("match_memories", {
                "query_embedding": query_embedding,
                "match_threshold": 0.7, # 70% similarity
                "match_count": limit
            }).execute()
            
            # 3. Extract content
            memories = [item['content'] for item in response.data]
            return memories
        except Exception as e:
            logger.error(f"Memory search failed: {e}")
            return []

memory_service = MemoryService()

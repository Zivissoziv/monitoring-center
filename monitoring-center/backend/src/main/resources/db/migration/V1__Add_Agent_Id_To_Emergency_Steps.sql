-- Migration to add agent_id column to emergency_steps table
-- This allows emergency steps to be executed on different agents

ALTER TABLE emergency_steps ADD COLUMN IF NOT EXISTS agent_id BIGINT;

-- Add comment to explain the column
COMMENT ON COLUMN emergency_steps.agent_id IS 'ID of the agent to execute on (null = use alert agent)';

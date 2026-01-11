-- Ensure only valid enum values exist
DELETE FROM saga_state
WHERE current_state NOT IN (
  'INITIATED',
  'COMPLETED',
  'FAILED',
  'FAILED_TIMEOUT'
);


CREATE TABLE submission_tracker (
  submission_id UUID NOT NULL,
  date_created  TIMESTAMP WITH TIME ZONE
);

CREATE OR REPLACE FUNCTION track_submissions()
  RETURNS TRIGGER AS
$BODY$
BEGIN
  INSERT INTO submission_tracker (submission_id, date_created) VALUES (new.uuid, new.last_modified);
  RETURN new;
END;
$BODY$
LANGUAGE plpgsql;

CREATE TRIGGER submission_tracker_trigger
  AFTER INSERT ON item
  FOR EACH ROW
  EXECUTE PROCEDURE track_submissions();
-- GIN indexes every element of the array and every key and value of the document, so
-- containment (@>) finds the few matching rows without reading the others.
CREATE INDEX properties_features ON properties USING gin (features);
CREATE INDEX properties_attrs ON properties USING gin (attrs jsonb_path_ops);

-- "Which network contains this address" is a containment question (>>=): GiST with inet_ops
-- (or SP-GiST, whose default operator class for inet is the same idea). A B-tree cannot help.
CREATE INDEX clinic_networks_net ON clinic_networks USING gist (net inet_ops);

-- At most one ACTIVE prescription of a drug per patient, whatever the case of its name:
-- a unique index that is both partial (WHERE active) and on an expression (lower(drug)).
CREATE UNIQUE INDEX prescriptions_one_active
    ON prescriptions (patient_id, lower(drug))
    WHERE active;

-- Every node of every plan, with how far its estimate was from reality in either direction,
-- and the worst node per plan.
SELECT DISTINCT ON (p.id)
       p.id,
       node ->> 'Node Type' AS node_type,
       node ->> 'Relation Name' AS relation_name,
       (node ->> 'Plan Rows')::bigint AS plan_rows,
       (node ->> 'Actual Rows')::bigint AS actual_rows
FROM captured_plans AS p,
     jsonb_path_query(p.plan, 'strict $.** ? (exists (@."Node Type"))') AS node
ORDER BY p.id,
         greatest((node ->> 'Plan Rows')::numeric, (node ->> 'Actual Rows')::numeric)
         / greatest(least((node ->> 'Plan Rows')::numeric, (node ->> 'Actual Rows')::numeric), 1) DESC;

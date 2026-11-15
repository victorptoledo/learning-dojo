# Quorum Store

Build the coordinator of a Dynamo-style store: every write goes to all reachable replicas and
succeeds with `W` acknowledgements, every read asks `R` replicas and returns the newest version,
and stale replicas are repaired on the way. The tests show both sides of `W + R > N`: the
overlap that hides a failed replica, and the stale read you get without it. Press **Enter** to
create the kata folder and open it.

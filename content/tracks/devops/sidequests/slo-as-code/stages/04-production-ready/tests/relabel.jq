# Applies Prometheus relabel_configs to one target, as Prometheus does for the actions
# keep, drop and replace: input {config: [...], labels: {...}}, output the labels, or null
# when the target is dropped. Regexes are anchored; separator ";", regex "(.*)",
# replacement "$1" and action "replace" are the defaults; an empty value removes a label.
def anchored($re): "^(?:" + $re + ")$";
def expand($m; $replacement):
  $replacement | gsub("\\$\\{?(?<n>[0-9]+)\\}?"; ($m.captures[(.n | tonumber) - 1].string // ""));
def apply($c):
  if . == null then null
  else
    . as $labels
    | ([($c.source_labels // [])[] | $labels[.] // ""] | join($c.separator // ";")) as $value
    | ($c.regex // "(.*)" | tostring) as $re
    | ($c.action // "replace") as $action
    | if $action == "keep" then (if ($value | test(anchored($re))) then $labels else null end)
      elif $action == "drop" then (if ($value | test(anchored($re))) then null else $labels end)
      elif $action == "replace" then
        ([$value | match(anchored($re))] | first) as $m
        | if $m == null then $labels
          else expand($m; ($c.replacement // "$1" | tostring)) as $new
            | if $new == "" then $labels | del(.[$c.target_label])
              else $labels + { ($c.target_label): $new } end
          end
      else error("the test simulator only knows keep, drop and replace, not \($action)")
      end
  end;
reduce .config[] as $c (.labels; apply($c))

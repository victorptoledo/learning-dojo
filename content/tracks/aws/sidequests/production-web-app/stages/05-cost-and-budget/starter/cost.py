"""What app.yaml costs a month, before anyone deploys it."""


def monthly_cost(template: dict, parameters: dict, prices: dict) -> dict:
    """{"lines": {logical id: dollars}, "total": dollars}, rounded to cents.

    `template` is app.yaml already parsed (intrinsics in their long form, {"Ref": "X"});
    `parameters` overrides the template's parameter defaults; `prices` is like prices.json.
    """
    raise NotImplementedError("todo: estimate the stack's monthly cost")


def fits_budget(template: dict, parameters: dict, prices: dict) -> bool:
    """Whether the estimate is within the template's MonthlyBudget."""
    raise NotImplementedError("todo: compare the estimate with MonthlyBudget")

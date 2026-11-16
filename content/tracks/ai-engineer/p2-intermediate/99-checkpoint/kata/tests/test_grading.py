import pytest

from llm import ScriptedModel
from rag import NOT_FOUND, SYSTEM, Answer, Doc, Passage, RagPipeline, citations, recall_at_k
from tokenizer import count_tokens

DOCS = [
    Doc(
        "travel",
        "Travel policy",
        "Meals during business trips are reimbursed up to 40 EUR per day.\n\n"
        "Trips abroad need approval from your manager two weeks before departure.",
    ),
    Doc(
        "leave",
        "Leave policy",
        "Employees have 25 days of holiday per year.\n\nUnused holiday days can be carried over until March.",
    ),
    Doc(
        "remote",
        "Remote work",
        "You may work from home up to three days per week.\n\n"
        "Home office equipment such as a monitor or chair is reimbursed up to 300 EUR once.\n\n"
        "Welcome to the team!",
    ),
    Doc(
        "salary",
        "Salary bands",
        "Salary bands are reviewed every April.\n\nRaises above the band need director approval.",
        audience="managers",
    ),
]


def pipeline(script=()):
    model = ScriptedModel(script)
    return RagPipeline(DOCS, model), model


# ---- citations ----------------------------------------------------------------------------------


def test_citations_handle_lists_spaces_and_repeats():
    assert citations("See [1,3] and [ 2 ] or [3, 1].") == [1, 3, 2]
    assert citations("[10] and [2]") == [10, 2]
    assert citations("an [a] or [] is not a citation") == []


# ---- retrieve -----------------------------------------------------------------------------------


def test_retrieve_finds_the_best_paragraph():
    rag, _ = pipeline()
    passages = rag.retrieve("Is a restaurant meal on a business trip reimbursed?")
    assert passages[0] == Passage(
        "travel#0",
        "travel",
        "Travel policy",
        "Meals during business trips are reimbursed up to 40 EUR per day.",
        pytest.approx(passages[0].score),
    )
    assert passages[0].score > 0.8
    assert len(passages) <= 3


def test_retrieve_respects_k_and_min_score_and_order():
    rag, _ = pipeline()
    passages = rag.retrieve("holiday days per year and unused days", k=2, min_score=0.0)
    assert [p.chunk_id for p in passages] == ["leave#0", "leave#1"]
    everything = rag.retrieve("holiday days per year and unused days", k=10, min_score=0.0)
    keys = [(-p.score, p.chunk_id) for p in everything]
    assert keys == sorted(keys)


def test_retrieve_hides_manager_documents_from_employees():
    rag, _ = pipeline()
    question = "When are salary bands reviewed?"
    assert all(p.doc_id != "salary" for p in rag.retrieve(question, k=10, min_score=0.0))
    assert rag.retrieve(question, role="manager")[0].doc_id == "salary"


def test_retrieve_skips_paragraphs_without_concepts_and_empty_queries():
    rag, _ = pipeline()
    ids = [p.chunk_id for p in rag.retrieve("home equipment", k=10, min_score=0.0)]
    assert "remote#2" not in ids
    assert rag.retrieve("Hello there") == []


# ---- build_prompt ---------------------------------------------------------------------------------


def test_build_prompt_numbers_the_sources():
    rag, _ = pipeline()
    passages = rag.retrieve("Can I expense a dinner on a trip abroad?", min_score=0.0)[:2]
    prompt, used = rag.build_prompt("Can I expense a dinner on a trip abroad?", passages, budget_tokens=500)
    lines = prompt.split("\n")
    assert lines[0] == "Sources:"
    assert lines[1] == f"[1] {passages[0].title}: {passages[0].text}"
    assert lines[2] == f"[2] {passages[1].title}: {passages[1].text}"
    assert lines[3:] == ["", "Question: Can I expense a dinner on a trip abroad?"]
    assert used == passages


def test_build_prompt_respects_the_budget():
    rag, _ = pipeline()
    passages = rag.retrieve("holiday days per year and unused days", k=2, min_score=0.0)
    full, _ = rag.build_prompt("q?", passages, budget_tokens=1_000)
    prompt, used = rag.build_prompt("q?", passages, budget_tokens=count_tokens(full) - 1)
    assert used == passages[:1]
    assert count_tokens(prompt) <= count_tokens(full) - 1
    empty, none = rag.build_prompt("q?", passages, budget_tokens=5)
    assert none == []
    assert empty == "Sources:\n\nQuestion: q?"


# ---- answer ----------------------------------------------------------------------------------------


def test_answer_with_valid_citations():
    rag, model = pipeline(["Yes, up to 40 EUR per day [1]. Trips abroad need approval [2]."])
    result = rag.answer("Is a dinner on a business trip abroad reimbursed?")
    assert result.status == "answered"
    assert result.citations == (1, 2)
    assert result.sources == ("travel",)
    request = model.requests[0]
    assert request.system == SYSTEM
    assert request.messages[0].content.startswith("Sources:\n[1] Travel policy: ")


def test_answer_not_found_without_calling_the_model():
    rag, model = pipeline()
    assert rag.answer("What is the weather like?") == Answer("not_found", NOT_FOUND, (), ())
    assert model.requests == []


def test_answer_when_the_model_says_not_found():
    rag, _ = pipeline([NOT_FOUND])
    assert rag.answer("How many holiday days do interns get?").status == "not_found"


def test_answer_repairs_invalid_citations_once():
    rag, model = pipeline(["Yes [1] and [4] and [7].", "Yes, 25 days per year [1]."])
    result = rag.answer("How many holiday days per year?")
    assert result == Answer("answered", "Yes, 25 days per year [1].", (1,), ("leave",))
    repair = model.requests[1].messages
    assert [m.role for m in repair] == ["user", "assistant", "user"]
    used = len(rag.build_prompt("How many holiday days per year?", rag.retrieve("How many holiday days per year?"))[1])
    assert repair[2].content == f"Your answer cited sources that do not exist: [4], [7]. Cite only sources [1] to [{used}]."


def test_answer_repairs_missing_citations():
    rag, model = pipeline(["You get 25 days.", "You get 25 days [1]."])
    assert rag.answer("How many holiday days per year?").status == "answered"
    assert model.requests[1].messages[2].content == "Your answer has no citations. Cite the sources you used as [n]."


def test_answer_unverified_after_one_repair():
    rag, model = pipeline(["Yes [9].", "Still [9] and [1]."])
    result = rag.answer("How many holiday days per year?")
    assert result.status == "unverified"
    assert result.text == "Still [9] and [1]."
    assert result.citations == (1,)
    assert result.sources == ("leave",)
    assert len(model.requests) == 2


def test_answer_respects_the_role():
    rag, model = pipeline(["Reviewed in April [1]."])
    result = rag.answer("When are salary bands reviewed?", role="manager")
    assert result.sources == ("salary",)
    employee, _ = pipeline([])
    assert employee.answer("When are salary bands reviewed?").status == "not_found"


def test_sources_are_unique_in_citation_order():
    rag, _ = pipeline(["Approval needed [2]. Meals [1]. Again [2]."])
    result = rag.answer("Do business trips abroad need approval, and are meals reimbursed?")
    assert result.citations == (2, 1)
    assert result.sources == ("travel",)


# ---- recall@k ----------------------------------------------------------------------------------------


def test_recall_at_k():
    rag, _ = pipeline()
    golden = [
        ("Are meals on business trips reimbursed?", "employee", "travel"),
        ("Can unused holiday days be carried over?", "employee", "leave"),
        ("Is a laptop or monitor reimbursed for home?", "employee", "remote"),
        ("When are salary bands reviewed?", "employee", "salary"),
    ]
    assert recall_at_k(rag, golden) == 0.75
    assert recall_at_k(rag, [(q, "manager", d) for q, _, d in golden]) == 1.0
    assert recall_at_k(rag, []) == 0.0

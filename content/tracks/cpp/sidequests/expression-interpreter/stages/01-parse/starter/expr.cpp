#include "expr.hpp"

namespace dojo {

std::expected<Expr, Error> parse(std::string_view text) {
    (void)text;
    return std::unexpected(Error{.position = 0, .kind = ErrorKind::UnexpectedEnd});
}

std::string to_string(const Expr& expr) {
    (void)expr;
    return "todo";
}

}  // namespace dojo

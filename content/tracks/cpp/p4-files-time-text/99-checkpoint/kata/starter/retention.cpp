#include "retention.hpp"

#include <stdexcept>

namespace dojo {

std::expected<Scan, ScanError> scan(const std::filesystem::path& dir,
                                    const std::chrono::time_zone* zone) {
    (void)dir;
    (void)zone;
    throw std::logic_error("todo: write scan");
}

Plan plan(std::vector<Backup> backups, const Policy& policy) {
    (void)backups;
    (void)policy;
    throw std::logic_error("todo: write plan");
}

std::string report(const Plan& plan) {
    (void)plan;
    throw std::logic_error("todo: write report");
}

std::expected<std::size_t, std::error_code> apply(const std::filesystem::path& dir,
                                                  const Plan& plan) {
    (void)dir;
    (void)plan;
    throw std::logic_error("todo: write apply");
}

}  // namespace dojo

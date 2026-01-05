// backend/src/blocker/filter.rs
pub struct UrlFilter;

impl UrlFilter {
    pub fn new() -> Self {
        Self
    }

    pub fn extract_domain(&self, url: &str) -> Option<String> {
        // Remove protocol
        let without_protocol = url
            .trim_start_matches("http://")
            .trim_start_matches("https://")
            .trim_start_matches("www.");

        // Extract domain before first /
        let domain = without_protocol
            .split('/')
            .next()?
            .split(':')
            .next()?
            .to_lowercase();

        Some(domain)
    }

    pub fn is_adult_content_pattern(&self, url: &str) -> bool {
        let adult_keywords = [
            "porn", "xxx", "sex", "adult", "nude", "nsfw",
            "xvideos", "pornhub", "xhamster", "redtube"
        ];

        let url_lower = url.to_lowercase();
        adult_keywords.iter().any(|keyword| url_lower.contains(keyword))
    }
}

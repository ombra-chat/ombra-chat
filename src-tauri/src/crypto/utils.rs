use rand::{distr::Alphanumeric, Rng};

const RANDOM_PASSWORD_LENGTH: usize = 25;

pub fn generate_random_password() -> String {
    let mut rng = rand::rng();
    std::iter::repeat_with(|| rng.sample(Alphanumeric) as char)
        .take(RANDOM_PASSWORD_LENGTH)
        .collect()
}

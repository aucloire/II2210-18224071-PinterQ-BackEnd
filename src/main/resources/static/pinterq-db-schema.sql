-- 1. Tabel Users (Kredensial dengan Username & Email)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabel Categories (Untuk fitur "Folder" Mata Kuliah)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabel Materials (Sumber teks yang diinput)
CREATE TABLE materials (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category_id INT REFERENCES categories(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabel Flashcards (Hasil Generate AI)
CREATE TABLE flashcards (
    id SERIAL PRIMARY KEY,
    material_id INT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    answer TEXT NOT NULL,
    is_memorized BOOLEAN DEFAULT FALSE
);

-- 5. Tabel Quizzes (Hasil Generate AI)
CREATE TABLE quizzes (
    id SERIAL PRIMARY KEY,
    material_id INT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    question TEXT NOT NULL,
    option_a VARCHAR(255) NOT NULL,
    option_b VARCHAR(255) NOT NULL,
    option_c VARCHAR(255) NOT NULL,
    option_d VARCHAR(255) NOT NULL,
    correct_answer VARCHAR(1) NOT NULL -- Simpan 'A', 'B', 'C', atau 'D'
);

-- 6. Tabel Quiz Attempts (Track Progress)
CREATE TABLE quiz_attempts (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    material_id INT NOT NULL REFERENCES materials(id) ON DELETE CASCADE,
    score NUMERIC(5, 2) NOT NULL,
    attempt_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
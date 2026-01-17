# Tasks for Non-Developer Using Cursor

This document contains well-defined tasks suitable for someone who is not a software developer but will use Cursor (AI-powered code editor) to complete them. All tasks are designed to be:
- **Safe** - Won't break the system
- **Well-defined** - Clear requirements and expected outcomes
- **AI-assisted** - Cursor can help guide through the work
- **Educational** - Opportunity to learn
- **Valuable** - Actually useful for the project

---

## 🟢 Beginner Level Tasks (Start Here)

### 1. Documentation Improvements
**Difficulty:** ⭐ Easy  
**Risk:** ✅ Very Low  
**Estimated Time:** 2-4 hours

**Task:** Review and improve existing documentation files

**Instructions:**
1. Read through `MOBILE_API_DOCUMENTATION.md`
2. Check for:
   - Typos and grammar errors
   - Missing information
   - Unclear explanations
   - Broken links or examples
3. Add more examples if needed
4. Improve formatting for better readability

**How Cursor helps:** Ask Cursor to "check this documentation for typos and improve clarity"

**Deliverable:** Updated documentation file with improvements

---

### 2. Add Code Comments
**Difficulty:** ⭐ Easy  
**Risk:** ✅ Very Low  
**Estimated Time:** 3-5 hours

**Task:** Add helpful comments to Java code files

**Instructions:**
1. Open Java files in `src/main/java/com/tiktel/ttelgo/`
2. Look for methods without comments
3. Ask Cursor: "Add JavaDoc comments to this method explaining what it does"
4. Review and ensure comments are accurate

**Files to start with:**
- `src/main/java/com/tiktel/ttelgo/esim/api/EsimController.java`
- `src/main/java/com/tiktel/ttelgo/plan/api/PlanController.java`

**How Cursor helps:** Cursor can generate JavaDoc comments automatically

**Deliverable:** Code files with improved documentation comments

---

### 3. Create API Response Examples
**Difficulty:** ⭐ Easy  
**Risk:** ✅ Very Low  
**Estimated Time:** 2-3 hours

**Task:** Create realistic JSON response examples for all APIs

**Instructions:**
1. Test each API endpoint using Postman or curl
2. Copy the actual responses
3. Create example files: `examples/api-responses/list-bundles-example.json`
4. Format them nicely with proper indentation
5. Add comments explaining each field

**How Cursor helps:** Ask Cursor to "format this JSON and add comments explaining each field"

**Deliverable:** Folder with example JSON files for each API

---

### 4. Update README.md
**Difficulty:** ⭐ Easy  
**Risk:** ✅ Very Low  
**Estimated Time:** 2-3 hours

**Task:** Make the README.md more user-friendly and complete

**Instructions:**
1. Read the current `README.md`
2. Add:
   - Quick start guide
   - Prerequisites checklist
   - Common issues and solutions
   - Links to important documentation
3. Improve formatting and structure

**How Cursor helps:** Ask Cursor to "improve this README with better structure and add a quick start section"

**Deliverable:** Updated README.md file

---

## 🟡 Intermediate Level Tasks

### 5. Create Test Data Scripts
**Difficulty:** ⭐⭐ Medium  
**Risk:** ✅ Low (test data only)  
**Estimated Time:** 4-6 hours

**Task:** Create SQL scripts to insert test data for development

**Instructions:**
1. Create a file: `src/main/resources/db/migration/test-data/V999__test_data.sql`
2. Add INSERT statements for:
   - Sample users
   - Sample bundles (if needed)
   - Sample orders (for testing)
3. Make sure data is realistic but clearly marked as test data

**How Cursor helps:** Ask Cursor to "generate SQL INSERT statements for test users with realistic data"

**Example:**
```sql
-- Test Users
INSERT INTO users (email, password, first_name, last_name, role) VALUES
('test.user1@example.com', 'hashed_password', 'John', 'Doe', 'USER'),
('test.user2@example.com', 'hashed_password', 'Jane', 'Smith', 'USER');
```

**Deliverable:** SQL file with test data

---

### 6. Create API Testing Scripts
**Difficulty:** ⭐⭐ Medium  
**Risk:** ✅ Low  
**Estimated Time:** 3-4 hours

**Task:** Create simple Python or PowerShell scripts to test APIs

**Instructions:**
1. Look at existing `test-api.py` as reference
2. Create new scripts:
   - `test-list-bundles.py` - Test list bundles API
   - `test-get-bundle-details.py` - Test get bundle details
   - `test-create-order.py` - Test create order
   - `test-get-qr-code.py` - Test get QR code
3. Each script should:
   - Make the API call
   - Print the response
   - Check if response is successful
   - Show any errors

**How Cursor helps:** Ask Cursor to "create a Python script to test the list bundles API endpoint"

**Deliverable:** Set of test scripts for each API

---

### 7. Add Error Message Improvements
**Difficulty:** ⭐⭐ Medium  
**Risk:** ✅ Low  
**Estimated Time:** 4-5 hours

**Task:** Improve error messages to be more user-friendly

**Instructions:**
1. Open `src/main/java/com/tiktel/ttelgo/common/exception/GlobalExceptionHandler.java`
2. Find error messages that are too technical
3. Ask Cursor to "make these error messages more user-friendly and less technical"
4. Review and ensure messages are still accurate

**Example:**
- Before: "JDBC exception executing SQL"
- After: "Database connection error. Please try again later."

**How Cursor helps:** Cursor can suggest better error messages

**Deliverable:** Updated error messages in exception handler

---

### 8. Create Configuration Guide
**Difficulty:** ⭐⭐ Medium  
**Risk:** ✅ Very Low  
**Estimated Time:** 3-4 hours

**Task:** Create a guide for configuring the application

**Instructions:**
1. Read `application.yml` and `application-prod.yml`
2. Create `CONFIGURATION_GUIDE.md` explaining:
   - What each configuration option does
   - How to set environment variables
   - Common configuration scenarios
   - Examples for different environments

**How Cursor helps:** Ask Cursor to "explain what each configuration in this YAML file does"

**Deliverable:** Configuration guide document

---

## 🟠 Advanced Beginner Tasks

### 9. Add Input Validation Messages
**Difficulty:** ⭐⭐⭐ Medium-Hard  
**Risk:** ✅ Low  
**Estimated Time:** 5-6 hours

**Task:** Add validation annotations and custom error messages

**Instructions:**
1. Find DTO classes (Data Transfer Objects) in the codebase
2. Look for classes like `ActivateBundleRequest.java`
3. Add validation annotations:
   - `@NotNull` - Field is required
   - `@Min` / `@Max` - Number ranges
   - `@Size` - String length
   - `@Email` - Email format
4. Add custom error messages

**Example:**
```java
@NotNull(message = "Order type is required")
private String type;

@Min(value = 1, message = "Quantity must be at least 1")
private Integer quantity;
```

**How Cursor helps:** Ask Cursor to "add validation annotations to this class with helpful error messages"

**Deliverable:** DTO classes with validation annotations

---

### 10. Create Logging Improvements
**Difficulty:** ⭐⭐⭐ Medium-Hard  
**Risk:** ✅ Low  
**Estimated Time:** 4-5 hours

**Task:** Add more detailed logging to important methods

**Instructions:**
1. Find service classes (e.g., `EsimService.java`)
2. Look for methods that don't have enough logging
3. Add log statements for:
   - Method entry (with parameters)
   - Important steps
   - Method exit (with results)
   - Errors

**Example:**
```java
log.info("Creating order for bundle: {}, quantity: {}", bundleName, quantity);
log.debug("Calling eSIM-Go API...");
log.info("Order created successfully. Order ID: {}", orderId);
```

**How Cursor helps:** Ask Cursor to "add appropriate logging statements to this method"

**Deliverable:** Service classes with improved logging

---

### 11. Create Database Migration Documentation
**Difficulty:** ⭐⭐⭐ Medium-Hard  
**Risk:** ✅ Very Low  
**Estimated Time:** 3-4 hours

**Task:** Document all database migrations

**Instructions:**
1. Look at `src/main/resources/db/migration/` folder
2. Create `DATABASE_MIGRATIONS.md`
3. For each migration file, document:
   - What it does
   - Why it was needed
   - What tables/columns it creates/modifies
   - Any important notes

**How Cursor helps:** Ask Cursor to "explain what this SQL migration does"

**Deliverable:** Migration documentation file

---

## 🔵 Advanced Tasks (Requires More Guidance)

### 12. Create Unit Test Examples
**Difficulty:** ⭐⭐⭐⭐ Hard  
**Risk:** ✅ Low  
**Estimated Time:** 6-8 hours

**Task:** Create example unit tests for controllers

**Instructions:**
1. Look at existing test files in `src/test/java/`
2. Create a new test file for `EsimController`
3. Write tests for:
   - List bundles endpoint
   - Get bundle details endpoint
   - Create order endpoint (mock the service)
4. Use Cursor to help write test code

**How Cursor helps:** Ask Cursor to "create a unit test for this controller method using MockMvc"

**Deliverable:** Test files with example unit tests

---

### 13. Add API Rate Limiting Documentation
**Difficulty:** ⭐⭐⭐ Medium  
**Risk:** ✅ Very Low  
**Estimated Time:** 2-3 hours

**Task:** Document rate limiting configuration

**Instructions:**
1. Find rate limiting configuration in the code
2. Create `RATE_LIMITING_GUIDE.md`
3. Document:
   - What rate limiting is
   - Current limits
   - How to configure limits
   - How to test limits

**How Cursor helps:** Ask Cursor to "explain the rate limiting configuration in this code"

**Deliverable:** Rate limiting documentation

---

## 📋 Recommended Task Order

### Week 1 (Getting Started)
1. ✅ Update README.md (Task 4)
2. ✅ Documentation Improvements (Task 1)
3. ✅ Create API Response Examples (Task 3)

### Week 2 (Building Skills)
4. ✅ Add Code Comments (Task 2)
5. ✅ Create Configuration Guide (Task 8)
6. ✅ Create Test Data Scripts (Task 5)

### Week 3 (More Advanced)
7. ✅ Create API Testing Scripts (Task 6)
8. ✅ Add Error Message Improvements (Task 7)
9. ✅ Create Logging Improvements (Task 10)

### Week 4 (Advanced)
10. ✅ Add Input Validation Messages (Task 9)
11. ✅ Create Database Migration Documentation (Task 11)
12. ✅ Add API Rate Limiting Documentation (Task 13)

---

## 🎯 How to Use Cursor for These Tasks

### General Workflow:
1. **Open the file** you need to work on
2. **Select the code** or section you want to modify
3. **Ask Cursor** using `Ctrl+K` or `Cmd+K`:
   - "Add comments to this method"
   - "Improve this error message"
   - "Create a test script for this API"
   - "Explain what this code does"
4. **Review the suggestions** - Cursor will show you what to change
5. **Accept or modify** the suggestions
6. **Test your changes** if applicable

### Example Prompts for Cursor:

**For Documentation:**
- "Improve this documentation with better examples"
- "Add a troubleshooting section to this guide"
- "Make this explanation clearer for non-technical users"

**For Code:**
- "Add JavaDoc comments to this class"
- "Add logging statements to this method"
- "Add validation annotations to this DTO"

**For Scripts:**
- "Create a Python script to test the list bundles API"
- "Generate SQL INSERT statements for test users"
- "Create a PowerShell script to check API health"

---

## ⚠️ Important Guidelines

### DO:
✅ Work on one task at a time  
✅ Test your changes when possible  
✅ Ask for help if stuck  
✅ Review Cursor's suggestions before accepting  
✅ Commit changes frequently with clear messages  
✅ Read existing code to understand patterns  

### DON'T:
❌ Modify core business logic without guidance  
❌ Change database schema without approval  
❌ Remove existing functionality  
❌ Commit changes without testing  
❌ Work on production-critical files alone  
❌ Accept all Cursor suggestions blindly  

---

## 📞 When to Ask for Help

Ask for help if:
- You're not sure what a task means
- Cursor's suggestions don't make sense
- You're modifying something that seems important
- You encounter errors you don't understand
- You need clarification on requirements

---

## ✅ Task Completion Checklist

For each task, ensure:
- [ ] Code/documentation follows existing patterns
- [ ] Changes are tested (if applicable)
- [ ] Documentation is clear and accurate
- [ ] No typos or errors introduced
- [ ] Changes are committed with clear messages
- [ ] You understand what you changed

---

## 🎓 Learning Resources

While working on these tasks, you'll learn:
- **Java basics** - Reading and understanding Java code
- **API concepts** - How REST APIs work
- **Documentation** - Writing clear technical docs
- **Testing** - Creating test scripts
- **Git** - Version control basics
- **SQL** - Database queries and migrations

---

**Last Updated:** January 4, 2026  
**For Questions:** Contact the development team


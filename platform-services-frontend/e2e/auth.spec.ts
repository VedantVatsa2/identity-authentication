import { test, expect } from '@playwright/test'

test.describe('Authentication Flow E2E', () => {
  test('navigates to register page and validates inputs', async ({ page }) => {
    await page.goto('/register')
    await expect(page.getByRole('heading', { name: /create an account/i })).toBeVisible()

    await page.getByRole('button', { name: /register account/i }).click()
    await expect(page.getByText(/email is required/i)).toBeVisible()
    await expect(page.getByText(/password must be at least 8 characters/i)).toBeVisible()
  })

  test('navigates to verify email page and validates challenge ID and OTP', async ({ page }) => {
    await page.goto('/verify-email')
    await expect(page.getByRole('heading', { name: /verify email address/i })).toBeVisible()

    await page.getByRole('button', { name: /verify email/i }).click()
    await expect(page.getByText(/challenge id is required/i)).toBeVisible()
    await expect(page.getByText(/verification code is required/i)).toBeVisible()
  })

  test('navigates to login page and validates credentials', async ({ page }) => {
    await page.goto('/login')
    await expect(page.getByRole('heading', { name: /welcome back/i })).toBeVisible()

    await page.getByRole('button', { name: /sign in/i }).click()
    await expect(page.getByText(/email is required/i)).toBeVisible()
    await expect(page.getByText(/password is required/i)).toBeVisible()
  })

  test('unauthenticated user accessing root / is redirected to /login', async ({ page }) => {
    await page.goto('/')
    await expect(page).toHaveURL(/\/login$/)
  })
})

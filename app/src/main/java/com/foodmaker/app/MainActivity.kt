package com.foodmaker.app

import android.app.Activity
import android.os.Bundle
import android.print.PrintManager
import com.foodmaker.app.data.ContentRepository
import com.foodmaker.app.engine.GameSession
import com.foodmaker.app.model.Recipe
import com.foodmaker.app.print.RecipePrintAdapter
import com.foodmaker.app.ui.CookingView
import com.foodmaker.app.ui.MenuView

class MainActivity : Activity() {

    private lateinit var repository: ContentRepository
    private var showingMenu: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = ContentRepository(this)
        showMenu()
    }

    private fun showMenu() {
        showingMenu = true
        setContentView(
            MenuView(
                context = this,
                recipes = repository.recipes,
                parts = repository.parts,
                onRecipeSelected = ::startRecipe
            )
        )
    }

    private fun startRecipe(recipe: Recipe) {
        showingMenu = false
        val session = GameSession(recipe)
        setContentView(
            CookingView(
                context = this,
                session = session,
                parts = repository.parts,
                onBack = ::showMenu,
                onPrint = { printRecipe(recipe) }
            )
        )
    }

    private fun printRecipe(recipe: Recipe) {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        printManager.print(
            "FoodMaker ${recipe.title}",
            RecipePrintAdapter(this, recipe, repository.parts),
            null
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (showingMenu) finish() else showMenu()
    }
}

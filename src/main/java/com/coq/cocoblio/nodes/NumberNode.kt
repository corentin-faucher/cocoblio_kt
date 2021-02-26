@file:Suppress("unused", "EXPERIMENTAL_API_USAGE")

package com.coq.cocoblio.nodes

import com.coq.cocoblio.maths.Digits
import com.coq.cocoblio.R
import com.coq.cocoblio.maths.getHighestDecimal
import com.coq.cocoblio.maths.getTheDigitAt
import kotlin.math.abs
import kotlin.math.max

/** Noeud racine d'un nombre. Les enfants sont des Surfaces "digits".
 * (NumberNode pour ne pas interférer avec la class Number de Kotlin.) */
class NumberNode : Node {
    private var number: Int
    private var digitPngResID: Int
    private var unitDecimal: Int
    private var separator: Digits
    private var extraDigit: Digits?
    private var spacing: Float
    private var sepSpacing: Float
    private val showPlus: Boolean

    constructor(refNode: Node?, number: Int,
                x: Float, y: Float, height: Float, lambda: Float = 0f,
                unitDecimal: Int = 0, digitPngResID: Int = R.drawable.digits_black,
                separator: Digits = Digits.Dot, extraDigit: Digits? = null,
                spacing: Float = 0.83f, sepSpacing: Float = 0.5f, showPlus: Boolean = false
    ) : super(refNode, x, y, 1f, 1f, lambda) {
        this.digitPngResID = digitPngResID
        this.number = number
        this.unitDecimal = unitDecimal
        this.separator = separator
        this.extraDigit = extraDigit
        this.spacing = spacing
        this.sepSpacing = sepSpacing
        this.showPlus = showPlus
        scaleX.set(height)
        scaleY.set(height)

        update()
    }
    /** Constructeur de copie. */
    private constructor(refNode: Node?, toCloneNode: NumberNode,
                asParent: Boolean, asElderBigbro: Boolean
    ) : super(refNode, toCloneNode, asParent, asElderBigbro) {
        digitPngResID = toCloneNode.digitPngResID
        number = toCloneNode.number
        unitDecimal = toCloneNode.unitDecimal
        separator = toCloneNode.separator
        extraDigit = toCloneNode.extraDigit
        spacing = toCloneNode.spacing
        sepSpacing = toCloneNode.sepSpacing
        showPlus = toCloneNode.showPlus
        update()
    }
    override fun copy(refNode: Node?, asParent: Boolean, asElderBigbro: Boolean) : NumberNode {
        return NumberNode(refNode, this, asParent, asElderBigbro)
    }

    /** Init ou met à jour un noeud NumberNode
     * (Ajoute les descendants si besoin)
     * update openBranch s'il y a le flag "show". */
    fun update(newNumber: Int, newUnitDecimal: Int? = null, newSeparator: Digits? = null) {
        number = newNumber
        newUnitDecimal?.let { unitDecimal = it }
        newSeparator?.let {separator = it}

        update()
    }

    private fun update() {
        // 0. Init...
        val refSurf = TiledSurface(null, digitPngResID, 0f, 0f, 1f)
        refSurf.scaleX.set(spacing)
        val sq = Squirrel(this)
        val displayedNumber: UInt = abs(number).toUInt()
        val isNegative = number < 0
        val maxDigits = max(displayedNumber.getHighestDecimal(), unitDecimal)

        // 1. Signe "-"
        sq.goDownForced(refSurf)
        if (isNegative) {
            (sq.pos as? TiledSurface)?.updateTile(Digits.Minus.ordinal, 0)
            sq.goRightForced(refSurf)
        } else if (showPlus) {
            (sq.pos as? TiledSurface)?.updateTile(Digits.Plus.ordinal, 0)
            sq.goRightForced(refSurf)
        }
        // 2. Chiffres avant le "separator"
        for (i in maxDigits downTo unitDecimal) {
            (sq.pos as? TiledSurface)?.updateTile(displayedNumber.getTheDigitAt(i).toInt(), 0)
            if(i > 0)
                sq.goRightForced(refSurf)
        }
        // 3. Separator et chiffres restants
        if(unitDecimal > 0) {
            (sq.pos as? TiledSurface)?.updateTile(separator.ordinal, 0)
            sq.pos.scaleX.set(sepSpacing)
            sq.goRightForced(refSurf)
            for (i in unitDecimal-1 downTo 0) {
                (sq.pos as? TiledSurface)?.updateTile(displayedNumber.getTheDigitAt(i).toInt(), 0)
                if(i > 0)
                    sq.goRightForced(refSurf)
            }
        }
        // 4. Extra/"unit" digit
        extraDigit?.let {
            sq.goRightForced(refSurf)
            (sq.pos as? TiledSurface)?.updateTile(it.ordinal, 0)
        }
        // 5. Nettoyage de la queue.
        while (sq.pos.littleBro != null) {
            sq.pos.disconnectBro(false)
        }
        // 6. Alignement
        alignTheChildren(0)
        // 7. Vérifier s'il faut afficher... (live update)
        if(containsAFlag(Flag1.show)) {
            openBranch()
        }
    }
}
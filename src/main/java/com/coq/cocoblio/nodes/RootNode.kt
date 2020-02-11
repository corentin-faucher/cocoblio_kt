package com.coq.cocoblio.nodes

import com.coq.cocoblio.maths.Vector3
import com.coq.cocoblio.maths.getLookAt


/** Noeud racine de la structure affichable.
 * width et height sont ajuster par le renderer pour correspondre à la région utilisable
 * de l'écran (sans les bord).
 * Le renderer met à jour à chaque frame les vrai dimension de la vue dans fullWidth
 * et fullHeight. */
@Suppress("ConvertSecondaryConstructorToPrimary")
open class RootNode : Node, Reshapable {
    protected val lookAt = Vector3(0f, 0f, 0f)
    protected val up = Vector3(0f, 1f, 0f)
    var fullWidth: Float = 2f
    var fullHeight: Float = 2f

    constructor(refNode: Node? = null) : super(refNode, 0f, 0f, 4f, 4f, 10f,
        Flag1.exposed or Flag1.show or Flag1.branchToDisplay or
                Flag1.selectableRoot or Flag1.reshapableRoot) {
        z.set(4f)
    }

    fun setModelAsCamera() {
        piu.model = getLookAt(Vector3(x.pos, y.pos, z.pos), lookAt, up)
    }

    override fun reshape(): Boolean {
        return true
    }
}
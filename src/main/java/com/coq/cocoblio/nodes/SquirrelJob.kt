@file:Suppress("unused")

/*--------------------------------------------------------------*/
/*-- Quelques extensions (de Node) utilisant les Squirrels.   --*/
/*--------------------------------------------------------------*/

package com.coq.cocoblio.nodes

import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.maths.printerror

/** Ajouter des flags à une branche (noeud et descendents s'il y en a). */
fun Node.addBranchFlags(flags: Long) {
    addFlags(flags)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.addFlags(flags)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Retirer des flags à toute la branche (noeud et descendents s'il y en a). */
fun Node.removeBranchFlags(flags: Long) {
    removeFlags(flags)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.removeFlags(flags)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Retirer des flags à la loop de frère où se situe le noeud présent. */
fun Node.removeBroLoopFlags(flags: Long) {
    removeFlags(flags)
    var sq = Squirrel(this)
    while (sq.goRight()) {
        sq.pos.removeFlags(flags)
    }
    sq = Squirrel(this)
    while (sq.goLeft()) {
        sq.pos.removeFlags(flags)
    }
}

/** Ajouter/retirer des flags à une branche (noeud et descendents s'il y en a). */
fun Node.addRemoveBranchFlags(flagsAdded: Long, flagsRemoved: Long) {
    addRemoveFlags(flagsAdded, flagsRemoved)
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        sq.pos.addRemoveFlags(flagsAdded, flagsRemoved)
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch.")
                return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}
/** Ajouter un flag aux "parents" (pas au noeud présent). */
fun Node.addRootFlag(flag: Long) {
    val sq = Squirrel(parent ?: return)
    do {
        if (sq.pos.containsAFlag(flag)) {
            break
        } else {
            sq.pos.addFlags(flag)
        }
    } while (sq.goUp())
}

/**  Pour chaque noeud :
 * 1. Applique open pour les openable,
 * 2. exécute le lambda supplémentaire,
 * 3. ajoute "show" si non caché,
 * 4. visite si est une branche avec "show".
 * (show peut avoir été ajouté exterieurement) */
fun Node.openBranch(extraCheck: ((Node) -> Unit)? = null) {
    (this as? OpenableNode)?.open()
    extraCheck?.invoke(this)
    if (!containsAFlag(Flag1.hidden)) {
        addFlags(Flag1.show)
    }
    if (!containsAFlag(Flag1.show)) {
        return
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        (sq.pos as? OpenableNode)?.open()
        extraCheck?.invoke(sq.pos)
        if (!sq.pos.containsAFlag(Flag1.hidden)) {
            sq.pos.addFlags(Flag1.show)
        }
        if (sq.pos.containsAFlag(Flag1.show)) if (sq.goDown()) {
            continue
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de branch."); return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Enlever "show" aux noeud de la branche (sauf les alwaysShow) et appliquer la "closure". */
fun Node.closeBranch(extraCheck: ((Node) -> Unit)? = null) {
    if (!containsAFlag(Flag1.exposed)) {
        removeFlags(Flag1.show)
        extraCheck?.invoke(this)
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        if (!sq.pos.containsAFlag(Flag1.exposed)) {
            sq.pos.removeFlags(Flag1.show)
            extraCheck?.invoke(sq.pos)
        }
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de this."); return
            } else if (sq.pos === this) {return}
        }
    }
}

/** Recherche d'un noeud selectionnable dans le noeud présent. Retourne nil si rien trouvé. */
fun Node.searchNodeToSelect(absPos: Vector2, nodeToAvoid: Node?) : Node? {
    val relPos = parent?.relativePosOf(absPos) ?: absPos
    return searchNodeToSelectPrivate(relPos, nodeToAvoid)
}

/*-- Private stuff --*/
private fun Node.searchNodeToSelectPrivate(relPos: Vector2,
                                           nodeToAvoid: Node?) : Node? {
    val sq = Squirrel(this, relPos, Squirrel.RSI.Ones)
    var candidate: Node? = null

    // 1. Cas particulier pour le point de départ -> On ne peut pas aller au littleBro...
    if (sq.isIn && sq.pos.containsAFlag(Flag1.show) && (sq.pos !== nodeToAvoid)) {
        // 1. Possibilité trouvé
        if (sq.pos.containsAFlag(Flag1.selectable)) {
            candidate = sq.pos
            if (!sq.pos.containsAFlag(Flag1.selectableRoot)) {return candidate}
        }
        // 2. Aller en profondeur
        if (sq.pos.containsAFlag(Flag1.selectableRoot)) {
            if (!sq.goDownP()) return candidate
        } else {
            return candidate
        }
    } else {return candidate} // (return si on ne peut pas aller en profondeur...)

    while (true) {
        if (sq.isIn) if (sq.pos.containsAFlag(Flag1.show) && (sq.pos !== nodeToAvoid)) {
            // 1. Possibilité trouvé
            if (sq.pos.containsAFlag(Flag1.selectable)) {
                candidate = sq.pos
                if (!sq.pos.containsAFlag(Flag1.selectableRoot)) {
                    return candidate
                }
            }
            // 2. Aller en profondeur
            if (sq.pos.containsAFlag(Flag1.selectableRoot)) {
                if (sq.goDownP()) {
                    continue
                } else {
                    printerror("selectableRoot sans desc.")
                }
            }
        }
        // 3. Remonter, si plus de petit-frère
        while (!sq.goRight()) {
            if (!sq.goUpP()) {
                printerror("Pas de root."); return candidate
            } else if (sq.pos === this) {return candidate}
        }
    }
}



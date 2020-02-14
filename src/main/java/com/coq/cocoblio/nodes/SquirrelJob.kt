@file:Suppress("unused")

/*--------------------------------------------------------------*/
/*-- Quelques extensions (de Node) utilisant les Squirrels.   --*/
/*--------------------------------------------------------------*/

package com.coq.cocoblio.nodes

import com.coq.cocoblio.maths.Vector2
import com.coq.cocoblio.maths.printerror

/** Ajouter des flags à une branche (noeud et descendents s'il y en a). */
fun Node.addBranchFlags_(flags: Long) {
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
fun Node.removeBranchFlags_(flags: Long) {
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
 * 1. Applique open() pour les Openable,
 * 2. ajoute "show" si non caché,
 * (show peut être ajouté manuellement avant pour afficher une branche cachée)
 * 3. visite si est une branche avec "show".
 * (show peut avoir été ajouté exterieurement) */
fun Node.openBranch() {
    (this as? Openable)?.open()
    if (!containsAFlag(Flag1.hidden)) {
        addFlags(Flag1.show)
    }
    if (!containsAFlag(Flag1.show)) {
        return
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        (sq.pos as? Openable)?.open()
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
fun Node.closeBranch() {
    if (!containsAFlag(Flag1.exposed)) {
        removeFlags(Flag1.show)
        (this as? Closeable)?.close()
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        if (!sq.pos.containsAFlag(Flag1.exposed)) {
            sq.pos.removeFlags(Flag1.show)
            (sq.pos as? Closeable)?.close()
        }
        if (sq.goDown()) {continue}
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de this."); return
            } else if (sq.pos === this) {return}
        }
    }
}

fun Node.reshapeBranch() {
    if (!containsAFlag(Flag1.show)) {
        return
    }
    var needToreshapeChildren: Boolean = (this as? Reshapable)?.reshape() ?: false
    if (!containsAFlag(Flag1.reshapableRoot) || !needToreshapeChildren) {
        return
    }
    val sq = Squirrel(firstChild ?: return)
    while (true) {
        if (sq.pos.containsAFlag(Flag1.show)) {
            needToreshapeChildren = (sq.pos as? Reshapable)?.reshape() ?: false
            if (sq.pos.containsAFlag(Flag1.reshapableRoot) && needToreshapeChildren) {
                if (sq.goDown()) {
                    continue
                }
            }
        }
        while (!sq.goRight()) {
            if (!sq.goUp()) {
                printerror("Pas de root."); return
            } else if (sq.pos === this) {
                return
            }
        }
    }
}

/** Recherche d'un noeud selectionnable dans le noeud présent. Retourne nil si rien trouvé. */
fun Node.searchBranchForSelectable(absPos: Vector2, nodeToAvoid: Node?) : Node? {
    val relPos = parent?.relativePosOf(absPos) ?: absPos
    return searchBranchForSelectablePrivate(relPos, nodeToAvoid)
}

/*-- Private stuff --*/
private fun Node.searchBranchForSelectablePrivate(relPos: Vector2,
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



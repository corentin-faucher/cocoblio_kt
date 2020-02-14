package com.coq.cocoblio.nodes

import com.coq.cocoblio.maths.Vector2

/** KeyboardKey peut être lié à un noeud-bouton-"touche" ou simplement un event du clavier. */
interface KeyboardKey {
    val scancode: Int
    val keycode: Int
    val keymod: Int
    val isVirtual: Boolean
}

/** Pour les type de noeud devant être vérifié à l'ouverture. */
interface Openable {
    fun open()
}
/** Pour les type de noeud devant être vérifié à la fermeture. */
interface Closeable {
    fun close()
}

/** Pour les noeuds "déplaçable".
 * 1. On prend le noeud : "grab",
 * 2. On le déplace : "drag",
 * 3. On le relâche : "letGo".
 * Retourne s'il y a une "action / event".
 * Un noeud Draggable doit être dans une classe descendante de SelectableNode.
 * On utilise les flags selectable et selectableRoot pour les trouver.
 * (On peut être draggable mais pas actionable, e.g. le sliding menu.) */
interface Draggable {
    fun grab(posInit: Vector2) : Boolean
    fun drag(posNow: Vector2) : Boolean
    fun letGo(speed: Vector2?) : Boolean
}
/** Un noeud pouvant être activé (e.g. les boutons).
 * Un noeud Actionable doit être dans une classe descendante de SelectableNode.
 * On utilise les flags selectable et selectableRoot pour les trouver. */
interface Actionable {
    fun action()
}

/** Un noeud pouvant être reshapé (e.g. un screen).
 * (Reshape: ajustement des positions/dimensions en fonction du cadre du parent).
 * Return: True s'il y a eu changement du cadre, i.e. besoin d'un reshape pour les enfants. */
interface Reshapable {
    fun reshape() : Boolean
}
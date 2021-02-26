package com.coq.cocoblio.nodes

/** KeyboardKey peut être lié à un noeud-bouton-"touche" ou simplement un event du clavier. */
interface KeyboardKey {
    val scancode: Int
    val keycode: Int
    val keymod: Int
    val isVirtual: Boolean
}

/** Pour les type de noeud devant être vérifié à l'ouverture. */
//interface Openable {
//    fun open()
//}
/** Pour les type de noeud devant être vérifié à la fermeture. */
//interface Closeable {
//    fun close()
//}

/** Un noeud pouvant être activé (e.g. les boutons).
 * Un noeud Actionable doit être dans une classe descendante de SelectableNode.
 * On utilise les flags selectable et selectableRoot pour les trouver. */
//interface Actionable {
//    fun action()
//}

/** Un noeud pouvant être reshapé (e.g. un screen).
 * (Reshape: ajustement des positions/dimensions en fonction du cadre du parent).
 * Return: True s'il y a eu changement du cadre, i.e. besoin d'un reshape pour les enfants. */
//interface Reshapable {
//    fun reshape() : Boolean
//}
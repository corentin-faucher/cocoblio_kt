package com.coq.cocoblio.nodes

/*----------------------------------------------*/
/*-- Les flags de base pour l'état d'un noeud.--*/
/*----------------------------------------------*/

/** Les flags "de base" pour les noeuds. */
object Flag1 {
    const val show = 1L
    /** Noeud qui n'apparait pas quand on "open" */
    const val hidden = 2L
    /** Noeud qui ne disparait pas quand on "close" */
    const val exposed = 4L
    /** Pour les noeuds selectionable/activable (les boutons) */
    internal const val selectableRoot = 1L.shl(3)
    internal const val selectable = 1L.shl(4)
    /** Noeud qui apparaît en grossisant. */
    const val poping = 1L.shl(5)
    const val isRoot = 1L.shl(6)

    /*-- Pour les surfaces --*/
    /** Par défaut on ajuste la largeur pour respecter les proportion d'une image. */
    const val surfaceDontRespectRatio = 1L.shl(7)
    const val surfaceWithCeiledWidth = 1L.shl(8)

    /*-- Pour les ajustement de height/width du parent ou du frame --*/
    const val giveSizesToBigBroFrame = 1L.shl(9)
    const val giveSizesToParent = 1L.shl(10)

    /*-- Pour les screens --*/
    /** Lors du reshape, le screen réaligne les "blocs" (premiers descendants).
     * Par défaut on aligne, il faut préciser seulement si on ne veut PAS aligner. */
    const val dontAlignScreenElements = 1L.shl(11)
    const val persistentScreen = 1L.shl(12)

    /*-- Affichage de branche --*/
    /** Paur l'affichage. La branche a encore des descendant à afficher. */
    const val branchToDisplay = 1L.shl(13)

    /*-- Placements à l'ouverture --*/
    const val relativeToRight = 1L.shl(14)
    const val relativeToLeft = 1L.shl(15)
    const val relativeToTop = 1L.shl(16)
    const val relativeToBottom = 1L.shl(17)
    const val fadeInRight = 1L.shl(18)
    const val relativeFlags = relativeToRight or relativeToLeft or relativeToTop or relativeToBottom
    const val openFlags = relativeFlags or fadeInRight

    const val notToAlign = 1L.shl(19)

    /** Le premier flag pouvant être utilisé dans un projet spécifique. */
    const val firstCustomFlag = 1L.shl(20)
}

     V*=====================================================================
     V* MODIFICHE Ril.  T Au Descrizione
     V* gg/mm/aa  nn.mm i xx Breve descrizione
     V*=====================================================================
     V* 06/11/20  002323  BERNI Creato
     V*=====================================================================
      * ENTRY
      * . Function
     D U$FUNZ          S             10
      *---------------------------------------------------------------
     D $N              S             10  0
     D $A              S             10
      *---------------------------------------------------------------
     D* M A I N
      *---------------------------------------------------------------
     C     *ENTRY        PLIST
     C                   PARM                    U$FUNZ
      *
     C                   ADD       1             $N
     C                   EVAL      $N=$N-1
     C                   MOVEL     1             $N
     C                   EVAL      $A='A'
     C                   MOVEL     'B'           $A
      *
     C                   SETON                                        LR
      *---------------------------------------------------------------
    RD* Initial subroutine
      *--------------------------------------------------------------*
     C     *INZSR        BEGS
      *
      *
     C                   ENDSR

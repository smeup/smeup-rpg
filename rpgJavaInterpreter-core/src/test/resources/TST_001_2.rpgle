     V* ============================================================================================
     V* RPG program that changes *entry param value
     V* ============================================================================================
     V* Author: Mattia Bonardi
     V* Company: Smeup.UP spa
      *--------------------------------------------------------------------------------------------*
      * General variables
      *--------------------------------------------------------------------------------------------*
      * *entry value
     D VALUE           S            100
      * Log variable
     D LOG             S             30
      *--------------------------------------------------------------------------------------------*
     C     *ENTRY        PLIST
     C                   PARM                    VALUE
      *
     C                   EVAL      LOG='START TST_001_2'
     C     LOG           DSPLY
      *
     C                   EVAL      VALUE='HELLO ' + %TRIM(VALUE)
      *
     C                   EVAL      LOG='END TST_001_2'
     C     LOG           DSPLY
      *--------------------------------------------------------------------------------------------*
     D COUNT           S              1  0 INZ(0)
     C                   SETON                                        50
     C     START         TAG
     C                   EVAL      COUNT += 1
     C     COUNT         DSPLY
     C                   IF        COUNT > 3
     C                   SETOFF                                       50
     C                   ENDIF
     C   50              GOTO      START
     C                   SETON                                        LR

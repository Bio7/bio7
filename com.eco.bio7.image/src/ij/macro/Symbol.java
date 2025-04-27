package ij.macro;

/** Objects of this class are used as entries in the macro language symbol table. */
public class Symbol implements MacroConstants {
    public int type;
    public double value;
    public String str;

    Symbol(int token, String str) {
        type = token&0xffff;
        this.str = str;
    }

    Symbol(double value) {
        this.value = value;
    }

    int getFunctionType() {
        int t = 0;
        if (type>=300 && type<1000)
            t = PREDEFINED_FUNCTION;
        else if (type>=1000 && type<2000)
            t = NUMERIC_FUNCTION;
        else if (type>=2000 && type<3000)
            t = STRING_FUNCTION;
        else if (type>=3000 && type<4000)
            t = ARRAY_FUNCTION;
        else if (type>=4000 && type<5000)
            t =  VARIABLE_FUNCTION;
        return t;
    }

    public String toString() {
        return type+" "+value+" "+str;
    }

} // class Symbol

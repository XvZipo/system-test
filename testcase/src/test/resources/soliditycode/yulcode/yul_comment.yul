object "Test" {
    code {
        datacopy(0, dataoffset("runtime"), datasize("runtime"))
        return(0, datasize("runtime"))
    }

    object "runtime" {
        code {

            // #sourceLocation(
            //     file : 'RelaxedSL.yul' ,
            //     line : 10
            // )

            function sum(x, y) -> r {
                r := add(x, y)
            }

            let res := sum(2, 3)
            mstore(0x00, res)
            return(0x00, 0x20)
        }
    }
}
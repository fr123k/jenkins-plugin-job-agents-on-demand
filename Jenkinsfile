node ("master") {
    tests = [:]
    projects = (1..500).collect {
        number ->
            build(job:'job-agents-on-demand', quietPeriod:0, wait:false)

    }
    return
}

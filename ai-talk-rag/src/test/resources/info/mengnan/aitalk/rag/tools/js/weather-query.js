function execute(params) {
    var city = params.city;
    var unit = params.unit || "celsius";

    // 模拟天气数据(实际应用中应该调用真实的天气API)
    var weatherData = {
        "北京": { temp: 15, weather: "晴", humidity: 45 },
        "上海": { temp: 20, weather: "多云", humidity: 60 },
        "深圳": { temp: 25, weather: "雨", humidity: 80 },
        "New York": { temp: 18, weather: "Sunny", humidity: 50 },
        "London": { temp: 12, weather: "Cloudy", humidity: 70 }
    };

    var data = weatherData[city];
    if (!data) {
        return "抱歉,暂时无法查询 " + city + " 的天气信息";
    }

    var temperature = unit === "fahrenheit" ?
        (data.temp * 9/5 + 32).toFixed(1) + "°F" :
        data.temp + "°C";

    return "📍 " + city + " 的天气信息:\n" +
           "🌡️ 温度: " + temperature + "\n" +
           "☁️ 天气: " + data.weather + "\n" +
           "💧 湿度: " + data.humidity + "%";
}
<h1>What it does</h1>
<p>
    Our project is an interactive web application that allows users to compare the cost of living and average salary across different U.S. states based on their nationality.
    By selecting a state from an interactive SVG map and choosing their nationality, users receive detailed financial insights, helping them determine affordability.
    The platform fetches real-time cost and salary data, providing a visual comparison through a dynamically generated bar graph.
</p>

<h1>How we built it</h1>
<p>
    We developed this project using <b>Java (Spark framework) for the backend</b> and <b>HTML, CSS, and JavaScript for the frontend</b>.
    The backend processes user input, retrieves relevant data from CSV files, and returns formatted responses.
    The frontend features an interactive SVG map, allowing users to select states dynamically.
    We implemented <b>JavaScript (Fetch API)</b> to communicate with the backend, updating the UI accordingly.
    The data is preprocessed and stored in <b>CSV files</b>, which are parsed and analyzed by Java.
</p>

<h1>Challenges we ran into</h1>
<p>
    One of the key challenges we faced was ensuring accurate <b>data mapping between state abbreviations and full names</b>, 
    as well as handling <b>case inconsistencies</b> when retrieving salary data by nationality. 
    Additionally, aligning the <b>SVG map interaction with backend data processing</b> required meticulous debugging.
    We also worked on optimizing <b>page responsiveness and UI styling</b>, making sure that elements such as the comparison bar graph display properly.
</p>

<h1>What's next for the project</h1>
<p>
    We plan to <b>enhance data visualization</b> by incorporating <b>more detailed financial breakdowns</b>, such as rent, utilities, and transportation costs.
    Additionally, integrating <b>real-time economic data sources</b> (such as APIs for updated salary and cost statistics) would make the tool even more insightful.
    Another future improvement is <b>user authentication and session management</b>, allowing users to <b>save and compare</b> different financial scenarios for better planning.
</p>
